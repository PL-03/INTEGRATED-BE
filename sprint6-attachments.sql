-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
SET TIME_ZONE = '+00:00';
-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema integrated
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema integrated
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `integrated` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci ;
USE `integrated` ;

-- -----------------------------------------------------
-- Table `integrated`.`users`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `integrated`.`users` (
    `oid` CHAR(36) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `username` VARCHAR(50) NOT NULL,
    `email` VARCHAR(50) NOT NULL,
    `created_on` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_on` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`oid`),
    UNIQUE INDEX `UC_username` (`username` ASC) VISIBLE,
    UNIQUE INDEX `UC_email` (`email` ASC) VISIBLE)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `integrated`.`board`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `integrated`.`board` (
    `boardId` VARCHAR(10) NOT NULL,
    `name` VARCHAR(120) NOT NULL,
    `oid` VARCHAR(36) NOT NULL,
    `visibility` ENUM('PUBLIC', 'PRIVATE') NOT NULL DEFAULT 'PRIVATE',
    `createdOn` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatedOn` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`boardId`),
    INDEX `fk_board_user` (`oid` ASC) VISIBLE,
    CONSTRAINT `fk_board_user`
    FOREIGN KEY (`oid`)
    REFERENCES `integrated`.`users` (`oid`))
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `integrated`.`board_collaborators`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `integrated`.`board_collaborators` (
    `boardId` VARCHAR(10) NOT NULL,
    `userId` CHAR(36) NOT NULL,
    `accessLevel` ENUM('READ', 'WRITE', 'PENDING') NOT NULL,
    `addedOn` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(50) NOT NULL,
    PRIMARY KEY (`boardId`, `userId`),
    INDEX `fk_collaborator_user` (`userId` ASC) VISIBLE,
    CONSTRAINT `fk_collaborator_board`
    FOREIGN KEY (`boardId`)
    REFERENCES `integrated`.`board` (`boardId`),
    CONSTRAINT `fk_collaborator_user`
    FOREIGN KEY (`userId`)
    REFERENCES `integrated`.`users` (`oid`))
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `integrated`.`statusv3`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `integrated`.`statusv3` (
                                                       `statusId` INT NOT NULL AUTO_INCREMENT,
                                                       `statusName` VARCHAR(50) NOT NULL,
    `statusDescription` VARCHAR(200) NULL DEFAULT NULL,
    `boardId` VARCHAR(10) NOT NULL,
    `createdOn` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatedOn` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`statusId`),
    INDEX `fk_statusv3_board1_idx` (`boardId` ASC) VISIBLE,
    CONSTRAINT `fk_statusv3_board1`
    FOREIGN KEY (`boardId`)
    REFERENCES `integrated`.`board` (`boardId`))
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `integrated`.`taskv3`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `integrated`.`taskv3` (
                                                     `id` INT NOT NULL AUTO_INCREMENT,
                                                     `taskTitle` VARCHAR(100) NOT NULL,
    `taskDescription` VARCHAR(500) NULL DEFAULT NULL,
    `taskAssignees` VARCHAR(30) NULL DEFAULT NULL,
    `taskStatusId` INT NOT NULL DEFAULT '1',
    `createdOn` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatedOn` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `boardId` VARCHAR(10) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `id_UNIQUE` (`id` ASC) VISIBLE,
    INDEX `fk_taskv3_taskStatus_idx` (`taskStatusId` ASC) VISIBLE,
    INDEX `fk_taskv3_board1_idx` (`boardId` ASC) VISIBLE,
    CONSTRAINT `fk_taskv3_board1`
    FOREIGN KEY (`boardId`)
    REFERENCES `integrated`.`board` (`boardId`),
    CONSTRAINT `fk_taskv3_taskStatus`
    FOREIGN KEY (`taskStatusId`)
    REFERENCES `integrated`.`statusv3` (`statusId`))
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `integrated`.`file_storage`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `integrated`.`file_storage` (
                                                           `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                           `name` VARCHAR(255) NOT NULL,
    `type` VARCHAR(100) NOT NULL,
    `path` VARCHAR(500) NOT NULL,
    `added_on` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `task_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_file_storage_task` (`task_id` ASC) VISIBLE,
    CONSTRAINT `fk_file_storage_task`
    FOREIGN KEY (`task_id`)
    REFERENCES `integrated`.`taskv3` (`id`))
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;

USE `integrated` ;

-- -----------------------------------------------------
-- procedure DeleteBoard
-- -----------------------------------------------------

DELIMITER $$
USE `integrated`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `DeleteBoard`(IN boardIdToDelete VARCHAR(10))
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
END$$

DELIMITER ;

-- -----------------------------------------------------
-- procedure DeleteTask
-- -----------------------------------------------------

DELIMITER $$
USE `integrated`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `DeleteTask`(IN taskIdToDelete INT)
BEGIN
    -- First delete all files associated with the task
DELETE FROM file_storage WHERE task_id = taskIdToDelete;

-- Then delete the task
DELETE FROM taskv3 WHERE id = taskIdToDelete;
END$$

DELIMITER ;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
