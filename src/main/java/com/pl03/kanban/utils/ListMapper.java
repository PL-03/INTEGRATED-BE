package com.pl03.kanban.utils;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ListMapper {

    // Generic method for mapping a list of entities to DTOs
    public <T, U> List<U> mapList(List<T> sourceList, Class<U> targetClass, Function<T, U> mapperFunction) {
        return sourceList.stream()
                .map(mapperFunction)
                .collect(Collectors.toList());
    }

    // Generic method for mapping a single entity to a DTO
    public static <T, U> U map(T source, Function<T, U> mapper) {
        return mapper.apply(source);
    }
}

