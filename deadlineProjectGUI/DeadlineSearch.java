package deadlineProjectGUI;

import javax.swing.*;
import java.awt.Component;
import java.awt.Frame;

public List<Deadline> searchDeadlines(List<Deadline> deadlines,
                                      String keyword) {

    return deadlines.stream()
            .filter(d ->
                    d.getTitle().toLowerCase()
                            .contains(keyword.toLowerCase())
                    ||
                    d.getDescription().toLowerCase()
                            .contains(keyword.toLowerCase()))
            .toList();
}

