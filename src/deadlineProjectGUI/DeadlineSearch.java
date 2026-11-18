package deadlineProjectGUI;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DeadlineSearch {

    private DeadlineSearch() {
    }

    public static List<Deadline> searchDeadlines(List<Deadline> deadlines, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new ArrayList<>(deadlines);
        }
        String kw = keyword.trim().toLowerCase();

        return deadlines.stream()
                .filter(d -> d.getTitle() != null && d.getTitle().toLowerCase().contains(kw))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}