package ru.hh.school.unittesting.example.homework;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hh.school.unittesting.homework.LibraryManager;
import ru.hh.school.unittesting.homework.NotificationService;
import ru.hh.school.unittesting.homework.UserService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LibraryManagerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private LibraryManager libraryManager;

    private static String userId, bookId;

    @BeforeAll
    public static void setup() {
        userId = "user01";
        bookId = "book01";
    }

    @Test
    void testAddBook() {
        libraryManager.addBook(bookId, 1);
        assertEquals(libraryManager.getAvailableCopies(bookId), 1);

        libraryManager.addBook(bookId, 0);
        assertEquals(libraryManager.getAvailableCopies(bookId), 1);

        libraryManager.addBook(bookId, 2);
        assertEquals(libraryManager.getAvailableCopies(bookId), 3);
    }

    @Test
    void testBorrowBookWhenUserInactive() {
        when(userService.isUserActive(userId)).thenReturn(false);
        assertFalse(libraryManager.borrowBook("01", userId));
        verify(notificationService, times(1)).notifyUser(userId, "Your account is not active.");
    }

    @Test
    void testBorrowBookWhenUserActiveNoBook() {
        when(userService.isUserActive(userId)).thenReturn(true);
        assertFalse(libraryManager.borrowBook("01", userId));
        verify(notificationService, never()).notifyUser(any(), any());
    }

    @Test
    void testBorrowBookWhenUserActiveSuccess() {
        when(userService.isUserActive(userId)).thenReturn(true);
        libraryManager.addBook(bookId, 2);

        assertTrue(libraryManager.borrowBook(bookId, userId));
        assertTrue(libraryManager.borrowBook(bookId, userId));
        verify(notificationService, times(2)).notifyUser(userId, "You have borrowed the book: " + bookId);
        assertFalse(libraryManager.borrowBook(bookId, userId));
        assertEquals(libraryManager.getAvailableCopies(bookId), 0);
    }

    @Test
    void testReturnBookNoBook() {
        assertFalse(libraryManager.returnBook(bookId, userId));
        verify(notificationService, never()).notifyUser(any(), any());
    }

    @Test
    void testReturnBookSuccess() {
        libraryManager.addBook(bookId, 1);
        when(userService.isUserActive(userId)).thenReturn(true);
        libraryManager.borrowBook(bookId, userId);
        assertFalse(libraryManager.returnBook(bookId, "user02"));
        assertTrue(libraryManager.returnBook(bookId, userId));
        verify(notificationService, times(1)).notifyUser(userId, "You have returned the book: " + bookId);
    }

    @Test
    void testGetAvailableCopies() {
        assertEquals(libraryManager.getAvailableCopies(bookId), 0);
        libraryManager.addBook(bookId, 1);
        assertEquals(libraryManager.getAvailableCopies(bookId), 1);
        when(userService.isUserActive(userId)).thenReturn(true);
        libraryManager.borrowBook(bookId, userId);
        assertEquals(libraryManager.getAvailableCopies(bookId), 0);
        assertTrue(libraryManager.returnBook(bookId, userId));
        assertEquals(libraryManager.getAvailableCopies(bookId), 1);
    }

    @Test
    void testCalculateDynamicLateFeeIllegalArgumentExceptionIfOverdueDaysNegative() {
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> libraryManager.calculateDynamicLateFee(-1, false, false)
        );
        assertEquals("Overdue days cannot be negative.", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "0, false, false, 0",
            "1, false, false, 0.5", // 1 * 0.5
            "1, true, false, 0.75", // 1 * 0.5 * 1.5
            "1, false, true, 0.4", // 1 * 0.5 * 0.8
            "1, true, true, 0.6", // 1 * 0.5 * 1.5 * 0.8
            "2, false, false, 1", // 2 * 0.5
            "2, true, false, 1.5", // 2 * 0.5 * 1.
            "2, false, true, 0.8", // 2 * 0.5 * 0.8
            "2, true, true, 1.2", // 2 * 0.5 * 1.5 * 0.8
    })
    void testCalculateDynamicLateFeeSuccess(
        int overdueDays,
        boolean isBestseller,
        boolean isPremiumMember,
        double expectedDynamicLateFee
    ) {
        assertEquals(libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember), expectedDynamicLateFee);
    }

}
