package deadlineProjectGUI;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeadlineFilterTest {

    @Test
    void shouldFilterPriority() {

        Deadline low =
                new Deadline(1,"A","",
                        LocalDateTime.now(),
                        "alacsony",
                        null);

        Deadline high =
                new Deadline(2,"B","",
                        LocalDateTime.now(),
                        "nagy",
                        null);

        List<Deadline> result =
                DeadlineFilter.filterByPriority(
                        List.of(low, high),
                        "alacsony");

        assertEquals(1, result.size());
        assertEquals("alacsony",
                result.getFirst().getPriority());
    }

    @Test
    void shouldReturnOnlyCompleted() {

        Deadline done =
                new Deadline(
                        1,
                        "Done",
                        "",
                        LocalDateTime.now(),
                        "közepes",
                        LocalDate.now());

        Deadline pending =
                new Deadline(
                        2,
                        "Pending",
                        "",
                        LocalDateTime.now(),
                        "közepes",
                        null);

        List<Deadline> result =
                DeadlineFilter.filterCompleted(
                        List.of(done, pending));

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnOnlyPending() {

        Deadline done =
                new Deadline(
                        1,
                        "Done",
                        "",
                        LocalDateTime.now(),
                        "közepes",
                        LocalDate.now());

        Deadline pending =
                new Deadline(
                        2,
                        "Pending",
                        "",
                        LocalDateTime.now(),
                        "közepes",
                        null);

        List<Deadline> result =
                DeadlineFilter.filterPending(
                        List.of(done, pending));

        assertEquals(1, result.size());
        assertEquals("Pending",
                result.getFirst().getTitle());
    }
}