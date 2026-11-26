package deadlineProjectGUI;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeadlineSorterTest {

    @Test
    void sortByTitle() {

        List<Deadline> list = new ArrayList<>(
                List.of(
                        new Deadline(
                                1,
                                "Zebra",
                                "",
                                LocalDateTime.now(),
                                "közepes",
                                null),
                        new Deadline(
                                2,
                                "Alma",
                                "",
                                LocalDateTime.now(),
                                "közepes",
                                null)
                ));

        DeadlineSorter.sortByTitle(list);

        assertEquals(
                "Alma",
                list.getFirst().getTitle()
        );
    }

    @Test
    void sortByDate() {

        Deadline future =
                new Deadline(
                        1,
                        "Future",
                        "",
                        LocalDateTime.now().plusDays(2),
                        "közepes",
                        null);

        Deadline now =
                new Deadline(
                        2,
                        "Now",
                        "",
                        LocalDateTime.now(),
                        "közepes",
                        null);

        List<Deadline> list =
                new ArrayList<>(List.of(future, now));

        DeadlineSorter.sortByDate(list);

        assertEquals(
                "Now",
                list.getFirst().getTitle()
        );
    }
}