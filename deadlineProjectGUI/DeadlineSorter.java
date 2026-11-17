package deadlineProjectGUI;

import javax.swing.*;
import java.awt.Component;
import java.awt.Frame;

public void sortByDate(List<Deadline> deadlines) {
    deadlines.sort(Comparator.comparing(Deadline::getDate));
}

public void sortByPriority(List<Deadline> deadlines) {
    deadlines.sort(
        Comparator.comparingInt(Deadline::getPriority).reversed()
    );
}

public void sortByTitle(List<Deadline> deadlines) {
    deadlines.sort(
        Comparator.comparing(Deadline::getTitle)
    );
}