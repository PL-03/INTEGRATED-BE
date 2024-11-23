package com.pl03.kanban.services;


import com.pl03.kanban.dtos.BoardRequest;
import com.pl03.kanban.dtos.BoardResponse;
import com.pl03.kanban.dtos.CollaboratorRequest;
import com.pl03.kanban.dtos.CollaboratorResponse;

import java.util.List;
import java.util.Map;

public interface BoardService {
    BoardResponse createBoard(BoardRequest request, String ownerOid, String ownerName);

    BoardResponse getBoardById(String id, String ownerName);

    List<BoardResponse> getAllBoards(String requesterOid);

    BoardResponse updateBoardVisibility(String boardId, Map<String, String> updateRequest, String ownerOid);

    //    boolean isOwner(String boardId, String requesterOid);
    List<CollaboratorResponse> getBoardCollaborators(String boardId, String requesterOid);

    CollaboratorResponse getBoardCollaboratorByOid(String boardId, String collabOid, String requesterOid);

    CollaboratorResponse addBoardCollaborator(String boardId, CollaboratorRequest request, String ownerOid);

    CollaboratorResponse updateCollaboratorAccessRight(String boardId, String collabOid, String accessRight, String requesterOid);

    void removeCollaborator(String boardId, String collabOid, String requesterOid);
    CollaboratorResponse acceptInvitation(String boardId, String userOid);
    void declineInvitation(String boardId, String userOid);
}
