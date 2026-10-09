package io.allitov.todo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Представляет проект в менеджере задач.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    private int id;

    private String name = "";
}
