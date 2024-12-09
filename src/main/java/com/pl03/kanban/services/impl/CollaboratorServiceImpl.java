package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.CollaboratorRequest;
import com.pl03.kanban.dtos.CollaboratorResponse;
import com.pl03.kanban.exceptions.*;
import com.pl03.kanban.kanban_entities.Board;
import com.pl03.kanban.kanban_entities.BoardCollaborators;
import com.pl03.kanban.kanban_entities.BoardCollaboratorsId;
import com.pl03.kanban.kanban_entities.Users;
import com.pl03.kanban.kanban_entities.repositories.BoardCollaboratorsRepository;
import com.pl03.kanban.kanban_entities.repositories.BoardRepository;
import com.pl03.kanban.kanban_entities.repositories.UsersRepository;
import com.pl03.kanban.services.CollaboratorService;
import com.pl03.kanban.user_entities.User;
import com.pl03.kanban.user_entities.UserRepository;
import com.pl03.kanban.utils.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.pl03.kanban.services.impl.BoardServiceImpl.getBoardAndCheckAccess;

@Service
public class CollaboratorServiceImpl implements CollaboratorService {
    private final BoardRepository boardRepository;
    private final BoardCollaboratorsRepository boardCollaboratorsRepository;
    private final JavaMailSender javaMailSender;

    // This map stores the access right temporarily before it's accepted
    private static Map<String, BoardCollaborators.AccessRight> tempAccessRights = new HashMap<>(); // for accept invitation
    private static final String TEMP_ACCESS_RIGHTS_FILE = "tempAccessRights.json";

    static {
        loadTempAccessRightsFromFile(); // initialize when restart
    }

    private final UserRepository userRepository;
    private final UsersRepository usersRepository;
    private final WebUtils webUtils;

    @Autowired
    public CollaboratorServiceImpl(BoardRepository boardRepository, BoardCollaboratorsRepository boardCollaboratorsRepository, JavaMailSender javaMailSender, UserRepository userRepository, UsersRepository usersRepository, WebUtils webUtils) {
        this.boardRepository = boardRepository;
        this.boardCollaboratorsRepository = boardCollaboratorsRepository;
        this.javaMailSender = javaMailSender;
        this.userRepository = userRepository;
        this.usersRepository = usersRepository;
        this.webUtils = webUtils;
    }

    @Override
    public List<CollaboratorResponse> getBoardCollaborators(String boardId, String requesterOid) {
        getBoardAndCheckAccess(boardId, requesterOid, boardRepository, boardCollaboratorsRepository);

        List<BoardCollaborators> collaborators = boardCollaboratorsRepository.findByBoardId(boardId);

        return collaborators.stream()
                .map(collaborator -> {
                    String tempAssignedRight = String.valueOf(tempAccessRights.getOrDefault(
                            boardId + "-" + collaborator.getUser().getOid(), null));
                    return CollaboratorResponse.builder()
                            .oid(collaborator.getUser().getOid())
                            .name(collaborator.getName())
                            .email(collaborator.getEmail())
                            .accessRight(collaborator.getAccessRight().name())
                            .assignedAccessRight(tempAssignedRight != null ? tempAssignedRight : null)
                            .addedOn(collaborator.getAddedOn())
                            .build();
                })
                .collect(Collectors.toList());
    }


    @Override
    public CollaboratorResponse getBoardCollaboratorByOid(String boardId, String collabOid, String requesterOid) {
        getBoardAndCheckAccess(boardId, requesterOid, boardRepository, boardCollaboratorsRepository);

        BoardCollaborators collaborator = boardCollaboratorsRepository.findByBoardIdAndUserOid(boardId, collabOid)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found"));

        String tempAssignedRight = String.valueOf(tempAccessRights.getOrDefault(
                boardId + "-" + collaborator.getUser().getOid(), null));

        return CollaboratorResponse.builder()
                .oid(collaborator.getUser().getOid())
                .name(collaborator.getName())
                .email(collaborator.getEmail())
                .accessRight(collaborator.getAccessRight().name())
                .assignedAccessRight(tempAssignedRight != null ? tempAssignedRight : null)
                .addedOn(collaborator.getAddedOn())
                .build();
    }


    @Override
    public CollaboratorResponse addBoardCollaborator(String boardId, CollaboratorRequest request, String ownerOid) {
        // Fetch the board and check authorization first
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board not found"));

        if (!board.getUser().getOid().equals(ownerOid)) {
            throw new UnauthorizedAccessException("Only the board owner can add collaborators", null);
        }

        // Validation of the request after authorization
        if (request == null || request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new InvalidBoardFieldException("Collaborator's email must be provided", null);
        }

        if (request.getAccessRight() == null || request.getAccessRight().isEmpty()) {
            throw new InvalidBoardFieldException("Access right must be provided", null);
        }

        // Fetch user from the shared database
        User authenticatedUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ItemNotFoundException("User not found with email: " + request.getEmail()));

        Users users = usersRepository.findByEmail(authenticatedUser.getEmail())
                .orElse(null);

        if (users == null) { //if user is not in team's db yet
            users = new Users();
            users.setOid(authenticatedUser.getOid());
            users.setUsername(authenticatedUser.getUsername());
            users.setName(authenticatedUser.getName());
            users.setEmail(authenticatedUser.getEmail());
            usersRepository.save(users);
        }

        // Prevent adding the board owner as a collaborator
        if (users.getOid().equals(ownerOid)) {
            throw new ConflictException("Cannot add board owner as a collaborator");
        }

        // Check if the user is already a collaborator or pending collaborator
        if (boardCollaboratorsRepository.existsByBoardIdAndUserOid(boardId, users.getOid())) {
            throw new ConflictException("The user is already a collaborator or pending collaborator of this board");
        }

        // Store the accessRight temporarily in memory
        BoardCollaborators.AccessRight accessRight = BoardCollaborators.AccessRight.valueOf(request.getAccessRight().toUpperCase());
        tempAccessRights.put(boardId + "-" + users.getOid(), accessRight);

        // Save the updated map to the file
        saveTempAccessRightsToFile();

        // Create the PENDING collaborator
        BoardCollaborators collaborator = new BoardCollaborators();
        collaborator.setId(new BoardCollaboratorsId(boardId, users.getOid()));
        collaborator.setBoard(board);
        collaborator.setUser(users);
        collaborator.setAccessRight(BoardCollaborators.AccessRight.PENDING); // Set as PENDING initially
        collaborator.setName(users.getName());
        collaborator.setEmail(users.getEmail());
        boardCollaboratorsRepository.save(collaborator);

        // Send invitation email
        sendInvitationEmail(board, request, users);

        // Return response with assigned access right
        return CollaboratorResponse.builder()
                .oid(users.getOid())
                .name(users.getName())
                .email(users.getEmail())
                .accessRight(BoardCollaborators.AccessRight.PENDING.name())
                .assignedAccessRight(accessRight.name()) // Set assigned access right
                .addedOn(collaborator.getAddedOn())
                .build();
    }

    public void sendInvitationEmail(Board board, CollaboratorRequest request, Users users) {
        String subject = String.format("%s has invited you to collaborate with %s access right on %s",
                board.getUser().getName(), request.getAccessRight(), board.getName());

        // Use WebUtils to get the base URL
        String invitationLink = String.format("%s/board/%s/collab/invitations", webUtils.getBaseUrl(), board.getId());

        String emailBody = String.format(
                "Hi, %s,\n\n%s has invited you to collaborate on the board \"%s\" with %s access rights.\n\nClick the link below to accept or decline the invitation:\n%s\n\nThank you,\nITBKK-PL3",
                users.getName(), board.getUser().getName(), board.getName(), request.getAccessRight(), invitationLink);

        try {
            sendSimpleEmail(users.getEmail(), subject, emailBody);
        } catch (Exception e) {
            throw new EmailSendException(String.format("We could not send an email to %s. They can accept the invitation at %s",
                    users.getName(), invitationLink));
        }
    }

    @Override
    public CollaboratorResponse acceptInvitation(String boardId, String userOid, String requesterOid) {
        // Check if the requester is the invited user
        if (!userOid.equals(requesterOid)) {
            throw new UnauthorizedAccessException("Only the invited user can accept this invitation", null);
        }

        BoardCollaborators collaborator = boardCollaboratorsRepository.findByBoardIdAndUserOid(boardId, userOid)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found"));

        if (collaborator.getAccessRight() != BoardCollaborators.AccessRight.PENDING) {
            throw new ConflictException("Invitation has already been accepted or declined");
        }

        // Retrieve the original access right from the map
        BoardCollaborators.AccessRight originalAccessRight = tempAccessRights.get(boardId + "-" + userOid);
        if (originalAccessRight == null) {
            throw new ConflictException("Original access right not found for the collaborator");
        }

        // Update the access right to the original one
        collaborator.setAccessRight(originalAccessRight);

        // Save the updated collaborator
        boardCollaboratorsRepository.save(collaborator);

        // remove the entry from the map (if it's no longer needed)
        tempAccessRights.remove(boardId + "-" + userOid);
        saveTempAccessRightsToFile(); //save json file

        return mapToCollaboratorResponse(collaborator);
    }


    @Override
    public void declineInvitation(String boardId, String userOid, String requesterOid) {
        // Check if the requester is the invited user
        if (!userOid.equals(requesterOid)) {
            throw new UnauthorizedAccessException("Only the invited user can decline this invitation", null);
        }

        BoardCollaborators collaborator = boardCollaboratorsRepository.findByBoardIdAndUserOid(boardId, userOid)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found"));

        if (collaborator.getAccessRight() != BoardCollaborators.AccessRight.PENDING) {
            throw new ConflictException("Invitation has already been accepted or declined");
        }

        boardCollaboratorsRepository.delete(collaborator);
        tempAccessRights.remove(boardId + "-" + userOid);
        saveTempAccessRightsToFile();
    }

    private static void saveTempAccessRightsToFile() {
        try (FileWriter writer = new FileWriter(TEMP_ACCESS_RIGHTS_FILE)) {
            Gson gson = new Gson();
            gson.toJson(tempAccessRights, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save temp access rights to file", e);
        }
    }

    private static void loadTempAccessRightsFromFile() {
        try (FileReader reader = new FileReader(TEMP_ACCESS_RIGHTS_FILE)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, BoardCollaborators.AccessRight>>() {
            }.getType();
            tempAccessRights = gson.fromJson(reader, type);

            if (tempAccessRights == null) {
                tempAccessRights = new HashMap<>();
            }
        } catch (IOException e) {
            // File might not exist on the first run, so just initialize the map
            tempAccessRights = new HashMap<>();
        }
    }

    @Override
    public CollaboratorResponse updateCollaboratorAccessRight(String boardId, String collabOid, String accessRight, String requesterOid) {
        getBoardAndCheckOwnership(boardId, requesterOid);

        BoardCollaborators collaborator = boardCollaboratorsRepository.findByBoardIdAndUserOid(boardId, collabOid)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found"));

        if (!boardCollaboratorsRepository.existsByBoardIdAndUserOidAndAccessRightNot(boardId, collabOid, BoardCollaborators.AccessRight.PENDING)) {
            throw new ConflictException("This user is not a collaborator yet. The invitation is still pending"); //check for collaborator's access right != pending
        }

        if (accessRight == null || accessRight.isEmpty()) {
            throw new InvalidBoardFieldException("accessRight is required", null);
        }

        BoardCollaborators.AccessRight newAccessRight;
        try {
            newAccessRight = BoardCollaborators.AccessRight.valueOf(accessRight.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidBoardFieldException("Invalid access right. Must be READ or WRITE", null);
        }

        collaborator.setAccessRight(newAccessRight);
        BoardCollaborators updatedCollaborator = boardCollaboratorsRepository.save(collaborator);
        return mapToCollaboratorResponse(updatedCollaborator);
    }

    @Override
    public CollaboratorResponse updatePendingInvitationAccessRight(String boardId, String collabOid, String accessRight, String requesterOid) {
        // Validate board and requester authorization
        getBoardAndCheckOwnership(boardId, requesterOid);

        // Validate accessRight input
        if (accessRight == null || accessRight.isEmpty()) {
            throw new InvalidBoardFieldException("Access right is required", null);
        }

        BoardCollaborators.AccessRight newAccessRight;
        try {
            newAccessRight = BoardCollaborators.AccessRight.valueOf(accessRight.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidBoardFieldException("Invalid access right. Must be READ or WRITE", null);
        }

        // Construct the key for pending invitation
        String key = boardId + "-" + collabOid;

        // Check if the pending invitation exists
        if (!tempAccessRights.containsKey(key)) {
            throw new ItemNotFoundException("Assigned access right not found");
        }

        // Update the assigned access right in the in-memory map
        tempAccessRights.put(key, newAccessRight);

        // Save the updated map to the JSON file
        saveTempAccessRightsToFile();

        // Fetch pending collaborator details (simulate database-like response)
        BoardCollaborators pendingCollaborator = boardCollaboratorsRepository.findByBoardIdAndUserOid(boardId, collabOid)
                .orElseThrow(() -> new ItemNotFoundException("Pending collaborator not found"));

        // Build and return the response
        return CollaboratorResponse.builder()
                .oid(pendingCollaborator.getUser().getOid())
                .name(pendingCollaborator.getUser().getName())
                .email(pendingCollaborator.getUser().getEmail())
                .accessRight(pendingCollaborator.getAccessRight().name()) // Current state is still PENDING
                .assignedAccessRight(newAccessRight.name()) // Updated assigned access right
                .addedOn(pendingCollaborator.getAddedOn())
                .build();
    }


    @Override
    public void removeCollaborator(String boardId, String collabOid, String requesterOid) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board not found"));

        boolean isOwner = board.getUser().getOid().equals(requesterOid);

        if (!isOwner) {
            // if not owner, check if they are an existing collaborator trying to remove themselves
            if (collabOid.equals(requesterOid)) {
                // check if they are a collaborator before allowing self-removal
                boolean isExistingCollaborator = boardCollaboratorsRepository
                        .findByBoardIdAndUserOid(boardId, requesterOid)
                        .isPresent();

                if (!isExistingCollaborator) {
                    throw new UnauthorizedAccessException("You are not a collaborator on this board", null);
                }
            } else {
                throw new UnauthorizedAccessException("You don't have permission to remove this collaborator", null);
            }
        }

        BoardCollaborators collaborator = boardCollaboratorsRepository.findByBoardIdAndUserOid(boardId, collabOid)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found"));

        boardCollaboratorsRepository.delete(collaborator);
    }

    public void sendSimpleEmail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@intproj23.sit.kmutt.ac.th");
        message.setReplyTo("DO NOT REPLY <noreply@intproj23.sit.kmutt.ac.th>");
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        javaMailSender.send(message);
    }

    private void getBoardAndCheckOwnership(String boardId, String requesterOid) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board not found"));

        if (!board.getUser().getOid().equals(requesterOid)) {
            throw new UnauthorizedAccessException("Only the board owner can perform this action", null);
        }
    }

    private CollaboratorResponse mapToCollaboratorResponse(BoardCollaborators collaborator) {
        return CollaboratorResponse.builder()
                .oid(collaborator.getUser().getOid())
                .name(collaborator.getName())
                .email(collaborator.getEmail())
                .accessRight(collaborator.getAccessRight().name())
                .addedOn(collaborator.getAddedOn())
                .build();
    }
}
