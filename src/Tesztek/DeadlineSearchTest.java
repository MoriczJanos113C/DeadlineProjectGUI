package deadlineProjectGUI;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeadlineSearchTest {

    @Test
    void shouldFindMatchingTitle() {

        Deadline d1 = new Deadline(
                1,
                "Java beadandó",
                "",
                LocalDateTime.now(),
                "közepes",
                null);

        Deadline d2 = new Deadline(
                2,
                "Matematika",
                "",
                LocalDateTime.now(),
                "alacsony",
                null);

        List<Deadline> result =
                DeadlineSearch.searchDeadlines(
                        List.of(d1, d2),
                        "java");

        assertEquals(1, result.size());
        assertEquals("Java beadandó",
                result.getFirst().getTitle());
    }

    @Test
    void emptyKeywordReturnsAll() {

        List<Deadline> result =
                DeadlineSearch.searchDeadlines(
                        List.of(
                                new Deadline(1,"A","",
                                        LocalDateTime.now(),
                                        "közepes",null),
                                new Deadline(2,"B","",
                                        LocalDateTime.now(),
                                        "közepes",null)
                        ),
                        "");

        assertEquals(2, result.size());
    }
}