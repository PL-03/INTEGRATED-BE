//package com.pl03.kanban.utils;
//
//import com.pl03.kanban.models.Task;
//import jakarta.persistence.AttributeConverter;
//import jakarta.persistence.Converter;
//
//@Converter(autoApply = true)
//public class TaskStatusConverter implements AttributeConverter<Task.TaskStatus, String> {
//
//    @Override
//    public String convertToDatabaseColumn(Task.TaskStatus attribute) {
//        switch (attribute) {
//            case No_Status:
//                return "No Status";
//            case To_Do:
//                return "To Do";
//            case Doing:
//                return "Doing";
//            case Done:
//                return "Done";
//            default:
//                throw new IllegalArgumentException("Unknown value: " + attribute);
//        }
//    }
//
//    @Override
//    public Task.TaskStatus convertToEntityAttribute(String dbData) {
//        switch (dbData) {
//            case "No Status":
//                return Task.TaskStatus.No_Status;
//            case "To Do":
//                return Task.TaskStatus.To_Do;
//            case "Doing":
//                return Task.TaskStatus.Doing;
//            case "Done":
//                return Task.TaskStatus.Done;
//            default:
//                throw new IllegalArgumentException("Unknown value: " + dbData);
//        }
//    }
//}
