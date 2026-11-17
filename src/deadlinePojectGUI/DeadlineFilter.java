package deadlineProjectGUI;

import javax.swing.*;
import java.awt.Component;
import java.awt.Frame;

public List<Deadline> filterByPriority(List<Deadline> deadlines, int priority) {
    return deadlines.stream()
            .filter(d -> d.getPriority() == priority)
            .toList();
}

public List<Deadline> filterCompleted(List<Deadline> deadlines) {
    return deadlines.stream()
            .filter(Deadline::isDone)
            .toList();
}

public List<Deadline> filterPending(List<Deadline> deadlines) {
    return deadlines.stream()
            .filter(d -> !d.isDone())
            .toList();
}