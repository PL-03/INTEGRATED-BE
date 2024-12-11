package com.pl03.kanban.services;

import com.pl03.kanban.dtos.CollaboratorRequest;
import com.pl03.kanban.dtos.CollaboratorResponse;

import java.util.List;

public interface CollaboratorService {
    List<CollaboratorResponse> getBoardCollaborators(String boardId, String requesterOid);

    CollaboratorResponse getBoardCollaboratorByOid(String boardId, String collabOid, String requesterOid);

    CollaboratorResponse addBoardCollaborator(String boardId, CollaboratorRequest request, String ownerOid);

    CollaboratorResponse updateCollaboratorAccessRight(String boardId, String collabOid, String accessRight, String requesterOid);

    CollaboratorResponse updatePendingInvitationAccessRight(String boardId, String collabOid, String accessRight, String requesterOid);

    void removeCollaborator(String boardId, String collabOid, String requesterOid);

    CollaboratorResponse acceptInvitation(String boardId, String userOid, String requesterOid);

    void declineInvitation(String boardId, String userOid, String requesterOid);
}
