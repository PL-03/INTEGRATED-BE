package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.BoardRequest;
import com.pl03.kanban.dtos.BoardResponse;
import com.pl03.kanban.dtos.CollaboratorRequest;
import com.pl03.kanban.dtos.CollaboratorResponse;
import com.pl03.kanban.exceptions.*;
import com.pl03.kanban.kanban_entities.*;
import com.pl03.kanban.kanban_entities.repositories.BoardCollaboratorsRepository;
import com.pl03.kanban.kanban_entities.repositories.BoardRepository;
import com.pl03.kanban.kanban_entities.repositories.UsersRepository;
import com.pl03.kanban.services.BoardService;
import com.pl03.kanban.services.StatusService;
import com.pl03.kanban.user_entities.User;
import com.pl03.kanban.user_entities.UserRepository;
import com.pl03.kanban.utils.WebUtils;
import jakarta.validation.constraints.NotNull;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final UsersRepository usersRepository;
    private final UserRepository userRepository;
    private final BoardCollaboratorsRepository boardCollaboratorsRepository;
    private final ModelMapper modelMapper;
    private final StatusService statusService;
    private final JavaMailSender javaMailSender;

    private static final int MAX_BOARD_NAME_LENGTH = 120;

    // This map stores the access right temporarily before it's accepted
    private static final Map<String, BoardCollaborators.AccessRight> tempAccessRights = new HashMap<>(); // for accept invitation


    @Autowired
    public BoardServiceImpl(BoardRepository boardRepository, UsersRepository usersRepository, UserRepository userRepository, BoardCollaboratorsRepository boardCollaboratorsRepository, ModelMapper modelMapper, StatusService statusService, JavaMailSender javaMailSender) {
        this.boardRepository = boardRepository;
        this.usersRepository = usersRepository;
        this.userRepository = userRepository;
        this.boardCollaboratorsRepository = boardCollaboratorsRepository;
        this.modelMapper = modelMapper;
        this.statusService = statusService;
        this.javaMailSender = javaMailSender;
    }

    @Override
    @Transactional(transactionManager = "kanbanTransactionManager")
    public BoardResponse createBoard(BoardRequest request, String ownerOid, String ownerName) {
        // Check if user already has a board
        if (boardRepository.existsByUserOid(ownerOid)) {
            throw new InvalidBoardFieldException("User can create only 1 board", null);
        }
        // Validate the board name
        ErrorResponse errorResponse = validateBoardFields(request);
        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidBoardFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        // Trim the board name after validation
        String trimmedBoardName = request.getName().trim();
        request.setName(trimmedBoardName);

        // Fetch the user by OID
        Users users = usersRepository.findByOid(ownerOid)
                .orElseThrow(() -> new ItemNotFoundException("User with oid " + ownerOid + " does not exist"));

        // Map the request to the Board entity
        Board board = modelMapper.map(request, Board.class);
        board.setUser(users);  // Associate the user with the board

        // Ensure unique boardId
        do {
            board.generateUniqueId();
        } while (boardRepository.existsById(board.getId()));

        // Set default visibility to PRIVATE
        board.setVisibility(Board.Visibility.PRIVATE);

        // Save the board
        board = boardRepository.save(board);

        // Add default status to the board
        statusService.addDefaultStatus(board.getId());

        // Return the BoardResponse
        return createBoardResponse(board, ownerName);
    }


    @Override
    @Transactional(readOnly = true, transactionManager = "kanbanTransactionManager")
    public BoardResponse getBoardById(String id, String requesterOid) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board not found with id: " + id));

        // Check access conditions for the board
        boolean isPublic = board.getVisibility() == Board.Visibility.PUBLIC;
        boolean isOwner = requesterOid != null && board.getUser().getOid().equals(requesterOid);
        boolean isCollaborator = requesterOid != null && boardCollaboratorsRepository.existsByBoardIdAndUserOid(board.getId(), requesterOid);

        // If the board is public, the requester is the owner, or the requester is a collaborator, return the board
        if (isPublic || isOwner || isCollaborator) {
            return createBoardResponse(board, board.getUser().getName());
        }

        // If access is denied, throw UnauthorizedAccessException
        throw new UnauthorizedAccessException("Access to this private board is restricted", null);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "kanbanTransactionManager")
    public List<BoardResponse> getAllBoards(String requesterOid) {
        return boardRepository.findAll().stream()
                .filter(board ->
                        board.getVisibility() == Board.Visibility.PUBLIC || // Include public boards
                                (requesterOid != null && (
                                        board.getUser().getOid().equals(requesterOid) || // Include boards owned by the requester
                                                boardCollaboratorsRepository.existsByBoardIdAndUserOid(board.getId(), requesterOid)
                                ))              // Include boards where the requester is a collaborator
                )
                .map(board -> createBoardResponse(board, board.getUser().getName())) // Map to response
                .collect(Collectors.toList()); // Collect to list
    }

    @Override
        @Transactional(transactionManager = "kanbanTransactionManager")
    public BoardResponse updateBoardVisibility(String boardId, Map<String, String> updateRequest, String ownerOid) {
        // Fetch the board by id
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board not found with id: " + boardId));

        // Verify if the requester is the board owner
        if (!board.getUser().getOid().equals(ownerOid)) {
            throw new UnauthorizedAccessException("Only the board owner can change the visibility", null);
        }

        // Check if updateRequest is null or empty
        if (updateRequest == null || updateRequest.isEmpty()) {
            throw new InvalidBoardFieldException("Request body must not be empty", null);
        }

        String visibility = updateRequest.get("visibility");
        if (visibility == null || visibility.isEmpty()) {
            throw new InvalidBoardFieldException("Body must have 'visibility' value to update board's visibility", null);
        }

        // Convert visibility to uppercase for comparison, but keep original for error message if needed
        String uppercaseVisibility = visibility.toUpperCase();
        if (uppercaseVisibility.equals("PRIVATE") || uppercaseVisibility.equals("PUBLIC")) {
            board.setVisibility(Board.Visibility.valueOf(uppercaseVisibility));
        } else {
            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid visibility value",
                    "Validation error"
            );
            errorResponse.addValidationError("visibility", "Visibility must be 'PRIVATE' or 'PUBLIC'");
            throw new InvalidBoardFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        // Save and return the updated board
        Board updatedBoard = boardRepository.save(board);
        return createBoardResponse(updatedBoard, updatedBoard.getUser().getName());
    }

//    @Override
//    public boolean isOwner(String boardId, String userOid) {
//        Board board = boardRepository.findById(boardId)
//                .orElseThrow(() -> new ItemNotFoundException("Board not found with id: " + boardId));
//        return board.getUser().getOid().equals(userOid);
//    }

//    private BoardResponse createBoardResponse(Board board, String ownerName) {
//        BoardResponse response = modelMapper.map(board, BoardResponse.class);
//        response.setOwner(new BoardResponse.OwnerResponse(board.getUser().getOid(), ownerName));  // Get OID from user
//        return response;
//    }
    @Override
    public List<CollaboratorResponse> getBoardCollaborators(String boardId, String requesterOid) {
        getBoardAndCheckAccess(boardId, requesterOid, boardRepository, boardCollaboratorsRepository);

        List<BoardCollaborators> collaborators = boardCollaboratorsRepository.findByBoardId(boardId);
        return collaborators.stream()
                .map(this::mapToCollaboratorResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CollaboratorResponse getBoardCollaboratorByOid(String boardId, String collabOid, String requesterOid) {
        getBoardAndCheckAccess(boardId, requesterOid, boardRepository, boardCollaboratorsRepository);

        BoardCollaborators collaborator = boardCollaboratorsRepository.findByBoardIdAndUserOid(boardId, collabOid)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found"));

        return mapToCollaboratorResponse(collaborator);
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
        tempAccessRights.put(boardId + "-" + users.getOid(), BoardCollaborators.AccessRight.valueOf(request.getAccessRight().toUpperCase()));

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

        return mapToCollaboratorResponse(collaborator);
    }

    public void sendInvitationEmail(Board board, CollaboratorRequest request, Users users) {
        // Send invitation email
        String subject = String.format("%s has invited you to collaborate with %s access right on %s",
                board.getUser().getName(), request.getAccessRight(), board.getName());

        String invitationLink = String.format("%s/board/%s/collab/invitations", WebUtils.getBaseUrl(), board.getId());

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
    public CollaboratorResponse acceptInvitation(String boardId, String userOid) {
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

        return mapToCollaboratorResponse(collaborator);
    }


    @Override
    public void declineInvitation(String boardId, String userOid) {
        BoardCollaborators collaborator = boardCollaboratorsRepository.findByBoardIdAndUserOid(boardId, userOid)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found"));

        if (collaborator.getAccessRight() != BoardCollaborators.AccessRight.PENDING) {
            throw new ConflictException("Invitation has already been accepted or declined");
        }

        boardCollaboratorsRepository.delete(collaborator);
        tempAccessRights.remove(boardId + "-" + userOid);
    }


    @Override
    public CollaboratorResponse updateCollaboratorAccessRight(String boardId, String collabOid, String accessRight, String requesterOid) {
        getBoardAndCheckOwnership(boardId, requesterOid);

        BoardCollaborators collaborator = boardCollaboratorsRepository.findByBoardIdAndUserOid(boardId, collabOid)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found"));

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
    //    private Board getBoardAndCheckAccess(String boardId, String requesterOid) {
//        Board board = boardRepository.findById(boardId)
//                .orElseThrow(() -> new ItemNotFoundException("Board not found"));
//
//        if (board.getVisibility() != Board.Visibility.PUBLIC &&
//                !board.getUser().getOid().equals(requesterOid) &&
//                !boardCollaboratorsRepository.existsByBoardIdAndUserOid(boardId, requesterOid)) { //check is a collaborator or not
//            throw new UnauthorizedAccessException("Access to this board is restricted", null);
//        }
//
//        return board;
//    }
    private void getBoardAndCheckOwnership(String boardId, String requesterOid) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board not found"));

        if (!board.getUser().getOid().equals(requesterOid)) {
            throw new UnauthorizedAccessException("Only the board owner can perform this action", null);
        }

    }

    static void getBoardAndCheckAccess(String boardId, String userId, BoardRepository boardRepository, BoardCollaboratorsRepository boardCollaboratorsRepository) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        if (board.getVisibility() != Board.Visibility.PUBLIC &&
                !board.getUser().getOid().equals(userId) && //check ownership
                !boardCollaboratorsRepository.existsByBoardIdAndUserOid(boardId, userId)) { //check is a collaborator or not
            throw new UnauthorizedAccessException("Access to this board is restricted", null);
        }
    }

    @NotNull
    public static Board validateBoardAccessAndOwnerShip(String boardId, String userId, BoardRepository boardRepository, BoardCollaboratorsRepository boardCollaboratorsRepository) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        if (!board.getUser().getOid().equals(userId)) {
            // Check if the user is a collaborator with write access
            Optional<BoardCollaborators> collaborator = boardCollaboratorsRepository.findByBoardIdAndUserOid(boardId, userId);
            if (collaborator.isEmpty() || collaborator.get().getAccessRight() != BoardCollaborators.AccessRight.WRITE) {
                throw new UnauthorizedAccessException("Only the board owner or collaborators with WRITE access can perform this operation", null);
            }
        }

        return board;
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
    private BoardResponse createBoardResponse(Board board, String ownerName) {
        BoardResponse response = new BoardResponse();
        response.setId(board.getId());
        response.setName(board.getName());
        response.setVisibility(BoardResponse.Visibility.valueOf(board.getVisibility().name()));
        response.setOwner(new BoardResponse.OwnerResponse(board.getUser().getOid(), ownerName));

        List<BoardResponse.CollaboratorResponse> collaborators;
        if (board.getCollaborators() != null) {
            collaborators = board.getCollaborators().stream()
                    .map(collab -> new BoardResponse.CollaboratorResponse(
                            collab.getUser().getOid(),
                            collab.getName(),
                            collab.getEmail(),
                            collab.getAccessRight()
                    ))
                    .collect(Collectors.toList());
        } else {
            collaborators = new ArrayList<>(); // Empty list if there are no collaborators
        }
        response.setCollaborators(collaborators);

        return response;
    }

    // Validation method for the board name
    private ErrorResponse validateBoardFields(BoardRequest boardRequest) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation error. Check 'errors' field for details", "");

        // Validate board name
        if (boardRequest.getName() == null || boardRequest.getName().trim().isEmpty()) {
            errorResponse.addValidationError(BoardRequest.Fields.name, "Board name must not be null or empty");
        } else if (boardRequest.getName().trim().length() > MAX_BOARD_NAME_LENGTH) {
            errorResponse.addValidationError(BoardRequest.Fields.name, "Board name must be between 1 and " + MAX_BOARD_NAME_LENGTH + " characters");
        }

        // If there are no validation errors, return null
        if (errorResponse.getErrors().isEmpty()) {
            return null;
        }

        return errorResponse;
    }
}