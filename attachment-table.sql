-- Set Time Zone
SET TIME_ZONE = '+00:00';

-- Create Database if not exists
CREATE DATABASE IF NOT EXISTS integrated /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE integrated;

-- Disable foreign key checks for now
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;

-- Drop existing users table if exists
DROP TABLE IF EXISTS users;

-- Create the users table
CREATE TABLE users (
                       oid CHAR(36) NOT NULL, -- Universally unique identifier (UUID)
                       name VARCHAR(100) NOT NULL, -- User full name (leading and trailing whitespaces trimmed)
                       username VARCHAR(50) NOT NULL, -- User username (leading and trailing whitespaces trimmed)
                       email VARCHAR(50) NOT NULL, -- User email
                       created_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- User creation timestamp
                       updated_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- User last update timestamp
                       PRIMARY KEY (oid),
                       CONSTRAINT UC_username UNIQUE (username), -- Ensuring username is unique
                       CONSTRAINT UC_email UNIQUE (email) -- Ensuring email is unique
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Drop existing board table if exists
DROP TABLE IF EXISTS board;

-- Create the board table with visibility ENUM column
CREATE TABLE board (
                       boardId varchar(10) NOT NULL,
                       name varchar(120) NOT NULL,
                       oid varchar(36) NOT NULL,
                       visibility ENUM('PUBLIC', 'PRIVATE') NOT NULL DEFAULT 'PRIVATE', -- Visibility column with default value 'PRIVATE'
                       createdOn timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updatedOn timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       PRIMARY KEY (boardId),
                       CONSTRAINT fk_board_user FOREIGN KEY (oid) REFERENCES users(oid) -- Foreign key to users table
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Drop existing statusv3 table if exists
DROP TABLE IF EXISTS statusv3;

-- Create the statusv3 table
CREATE TABLE statusv3 (
                          statusId int NOT NULL AUTO_INCREMENT,
                          statusName varchar(50) NOT NULL,
                          statusDescription varchar(200) DEFAULT NULL,
                          boardId varchar(10) NOT NULL,
                          createdOn timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updatedOn timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          PRIMARY KEY (statusId),
                          KEY fk_statusv3_board1_idx (boardId),
                          CONSTRAINT fk_statusv3_board1 FOREIGN KEY (boardId) REFERENCES board (boardId)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Drop existing taskv3 table if exists
DROP TABLE IF EXISTS taskv3;

-- Create the taskv3 table
CREATE TABLE taskv3 (
                        id int NOT NULL AUTO_INCREMENT,
                        taskTitle varchar(100) NOT NULL,
                        taskDescription varchar(500) DEFAULT NULL,
                        taskAssignees varchar(30) DEFAULT NULL,
                        taskStatusId int NOT NULL DEFAULT '1',
                        createdOn timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updatedOn timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        boardId varchar(10) NOT NULL,
                        PRIMARY KEY (id),
                        UNIQUE KEY id_UNIQUE (id),
                        KEY fk_taskv3_taskStatus_idx (taskStatusId),
                        KEY fk_taskv3_board1_idx (boardId),
                        CONSTRAINT fk_taskv3_board1 FOREIGN KEY (boardId) REFERENCES board (boardId),
                        CONSTRAINT fk_taskv3_taskStatus FOREIGN KEY (taskStatusId) REFERENCES statusv3 (statusId)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Enable foreign key checks again
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
-- Set Time Zone
SET TIME_ZONE = '+00:00';

-- Create Database if not exists
CREATE DATABASE IF NOT EXISTS integrated /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE integrated;

-- Disable foreign key checks for now
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;

-- Drop existing users table if exists
DROP TABLE IF EXISTS users;

-- Create the users table
CREATE TABLE users (
                       oid CHAR(36) NOT NULL, -- Universally unique identifier (UUID)
                       name VARCHAR(100) NOT NULL, -- User full name (leading and trailing whitespaces trimmed)
                       username VARCHAR(50) NOT NULL, -- User username (leading and trailing whitespaces trimmed)
                       email VARCHAR(50) NOT NULL, -- User email
                       created_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- User creation timestamp
                       updated_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- User last update timestamp
                       PRIMARY KEY (oid),
                       CONSTRAINT UC_username UNIQUE (username), -- Ensuring username is unique
                       CONSTRAINT UC_email UNIQUE (email) -- Ensuring email is unique
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Drop existing board table if exists
DROP TABLE IF EXISTS board;

-- Create the board table with visibility ENUM column
CREATE TABLE board (
                       boardId VARCHAR(10) NOT NULL,
                       name VARCHAR(120) NOT NULL,
                       oid VARCHAR(36) NOT NULL,
                       visibility ENUM('PUBLIC', 'PRIVATE') NOT NULL DEFAULT 'PRIVATE', -- Visibility column with default value 'PRIVATE'
                       createdOn TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updatedOn TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       PRIMARY KEY (boardId),
                       CONSTRAINT fk_board_user FOREIGN KEY (oid) REFERENCES users(oid) -- Foreign key to users table
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Drop existing statusv3 table if exists
DROP TABLE IF EXISTS statusv3;

-- Create the statusv3 table
CREATE TABLE statusv3 (
                          statusId INT NOT NULL AUTO_INCREMENT,
                          statusName VARCHAR(50) NOT NULL,
                          statusDescription VARCHAR(200) DEFAULT NULL,
                          boardId VARCHAR(10) NOT NULL,
                          createdOn TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updatedOn TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          PRIMARY KEY (statusId),
                          KEY fk_statusv3_board1_idx (boardId),
                          CONSTRAINT fk_statusv3_board1 FOREIGN KEY (boardId) REFERENCES board (boardId)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Drop existing taskv3 table if exists
DROP TABLE IF EXISTS taskv3;

-- Create the taskv3 table
CREATE TABLE taskv3 (
                        id INT NOT NULL AUTO_INCREMENT,
                        taskTitle VARCHAR(100) NOT NULL,
                        taskDescription VARCHAR(500) DEFAULT NULL,
                        taskAssignees VARCHAR(30) DEFAULT NULL,
                        taskStatusId INT NOT NULL DEFAULT '1',
                        createdOn TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updatedOn TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        boardId VARCHAR(10) NOT NULL,
                        PRIMARY KEY (id),
                        UNIQUE KEY id_UNIQUE (id),
                        KEY fk_taskv3_taskStatus_idx (taskStatusId),
                        KEY fk_taskv3_board1_idx (boardId),
                        CONSTRAINT fk_taskv3_board1 FOREIGN KEY (boardId) REFERENCES board (boardId),
                        CONSTRAINT fk_taskv3_taskStatus FOREIGN KEY (taskStatusId) REFERENCES statusv3 (statusId)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Drop existing board_collaborators table if exists
DROP TABLE IF EXISTS board_collaborators;

-- Create the BoardCollaborators table
CREATE TABLE board_collaborators (
                                     boardId VARCHAR(10) NOT NULL,
                                     userId CHAR(36) NOT NULL, -- Reference to user oid
                                     accessLevel ENUM('READ', 'WRITE') NOT NULL, -- Access level
                                     addedOn TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Timestamp when collaborator was added
                                     name VARCHAR(100) NOT NULL, -- Collaborator name, same length as in users table
                                     email VARCHAR(50) NOT NULL, -- Collaborator email, same length as in users table
                                     PRIMARY KEY (boardId, userId),
                                     CONSTRAINT fk_collaborator_board FOREIGN KEY (boardId) REFERENCES board (boardId),
                                     CONSTRAINT fk_collaborator_user FOREIGN KEY (userId) REFERENCES users(oid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Enable foreign key checks again
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;

-- Drop existing file_storage table if exists
DROP TABLE IF EXISTS file_storage;

-- Create the file_storage table
CREATE TABLE file_storage (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              name VARCHAR(255) NOT NULL,
                              type VARCHAR(100) NOT NULL,
                              file_data LONGBLOB NOT NULL, -- Stores compressed file data
                              added_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              task_id INT NOT NULL,
                              PRIMARY KEY (id),
                              CONSTRAINT fk_file_storage_task FOREIGN KEY (task_id) REFERENCES taskv3 (id),
                              CONSTRAINT chk_file_size CHECK (LENGTH(file_data) <= 20971520) -- Limit file size to 20MB
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Modify the stored procedure to handle file deletions when deleting a task
DROP PROCEDURE IF EXISTS DeleteTask;
DELIMITER //
CREATE PROCEDURE DeleteTask(IN taskIdToDelete INT)
BEGIN
    -- First delete all files associated with the task
DELETE FROM file_storage WHERE task_id = taskIdToDelete;

-- Then delete the task
DELETE FROM taskv3 WHERE id = taskIdToDelete;
END //
DELIMITER ;

-- Update the DeleteBoard procedure to delete files related to tasks under the board
DROP PROCEDURE IF EXISTS DeleteBoard;
DELIMITER //
CREATE PROCEDURE DeleteBoard(IN boardIdToDelete VARCHAR(10))
BEGIN
    -- First delete all files associated with tasks under the board
    DELETE fs
    FROM file_storage fs
    INNER JOIN taskv3 t ON fs.task_id = t.id
    WHERE t.boardId = boardIdToDelete;

    -- Then delete all tasks associated with the board
DELETE FROM taskv3 WHERE boardId = boardIdToDelete;

-- Then delete all statuses associated with the board
DELETE FROM statusv3 WHERE boardId = boardIdToDelete;

-- Then delete all collaborators associated with the board
DELETE FROM board_collaborators WHERE boardId = boardIdToDelete;

-- Finally delete the board
DELETE FROM board WHERE boardId = boardIdToDelete;
END //
DELIMITER ;
