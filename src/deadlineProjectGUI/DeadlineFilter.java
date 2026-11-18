package deadlineProjectGUI;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DeadlineFilter {

    private DeadlineFilter() {
    }

    public static List<Deadline> filterByPriority(List<Deadline> deadlines, String priority) {
        if (priority == null || priority.equalsIgnoreCase("All") || priority.equalsIgnoreCase("Összes")) {
            return new ArrayList<>(deadlines);
        }
        return deadlines.stream()
                .filter(d -> d.getPriority() != null && d.getPriority().equalsIgnoreCase(priority))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static List<Deadline> filterCompleted(List<Deadline> deadlines) {
        return deadlines.stream()
                .filter(Deadline::isDone)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static List<Deadline> filterPending(List<Deadline> deadlines) {
        return deadlines.stream()
                .filter(d -> !d.isDone())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}