package deadlineProjectGUI;

import java.util.Comparator;
import java.util.List;

public class DeadlineSorter {

    private DeadlineSorter() {
    }

    public static void sortByDate(List<Deadline> deadlines) {
        deadlines.sort(Comparator.comparing(
                Deadline::getDate,
                Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public static void sortByPriority(List<Deadline> deadlines) {
        deadlines.sort(Comparator.comparingInt(
                (Deadline d) -> priorityRank(d.getPriority())).reversed());
    }

    public static void sortByTitle(List<Deadline> deadlines) {
        deadlines.sort(Comparator.comparing(
                Deadline::getTitle,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    }

    private static int priorityRank(String priority) {
        if (priority == null) {
            return 0;
        }
        return switch (priority.trim().toLowerCase()) {
            case "nagy" -> 3;
            case "közepes" -> 2;
            case "alacsony" -> 1;
            default -> 0;
        };
    }
}