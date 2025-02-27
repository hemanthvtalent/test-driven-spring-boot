package com.xpinjection.library.service;

import com.xpinjection.library.adaptors.persistence.BookDao;
import com.xpinjection.library.domain.Book;
import com.xpinjection.library.domain.Books;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

/**
 * @author Alimenkou Mikalai
 */
@ExtendWith(MockitoExtension.class)
public class BookServiceImplTest {
    @Mock
    private BookDao dao;

    private BookService bookService;

    @BeforeEach
    void init() {
        bookService = new BookServiceImpl(dao);
    }

    @Test
    void ifNoBooksPassedEmptyListIsReturned() {
        assertThat(bookService.addBooks(Books.empty())).isEmpty();
    }

    @Test
    void forEveryPairOfTitleAndAuthorBookIsCreatedAndStored() {
        var first = new Book("The first", "author");
        var second = new Book("The second", "another author");
        when(dao.save(notNull())).thenReturn(first).thenReturn(second);

        Map<String, String> books = new HashMap<>();
        books.put("The first", "author");
        books.put("The second", "another author");
        assertThat(bookService.addBooks(Books.fromMap(books))).contains(first, second);
    }

    @Test
    void ifNoBooksFoundForAuthorReturnEmptyList() {
        when(dao.findByAuthor("a")).thenReturn(emptyList());

        assertNoBooksFound("a");
        verify(dao, only()).findByAuthor("a");
    }

    @Test
    void ifAuthorIsEmptyThrowException() {
        assertThatThrownBy(() -> bookService.findBooksByAuthor(" \t \n "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ifAuthorIsNullThrowException() {
        assertThatThrownBy(() -> bookService.findBooksByAuthor(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void booksByAuthorShouldBeCached() {
        var book = new Book("The book", "author");
        when(dao.findByAuthor("a")).thenReturn(singletonList(book));
        when(dao.findByAuthor("a a")).thenReturn(emptyList());

        assertBooksByAuthor("a", book);
        assertBooksByAuthor("a", book);
        assertNoBooksFound("a a");
        verify(dao, times(1)).findByAuthor("a");
    }

    @Test
    void ifCamelCaseDetectedThenSplitInvalidAuthorNameOnFirstAndLastName() {
        var book = new Book("The book", "Mikalai Alimenkou");
        when(dao.findByAuthor("Mikalai Alimenkou")).thenReturn(singletonList(book));

        assertBooksByAuthor("MikalaiAlimenkou", book);
    }

    @Test
    void punctuationShouldBeIgnored() {
        var book = new Book("The book", "Who cares");
        when(dao.findByAuthor("Who cares?")).thenReturn(singletonList(book));

        assertBooksByAuthor("Who cares?", book);
    }

    @Test
    void compositeLastNameIsNotSplit() {
        var book = new Book("The book", "Alfred McGregor");
        when(dao.findByAuthor("Alfred McGregor")).thenReturn(singletonList(book));

        assertBooksByAuthor("Alfred McGregor", book);
    }

    @Test
    void authorNameShouldBeTrimmedBeforeUsage() {
        var book = new Book("The book", "Mikalai Alimenkou");
        when(dao.findByAuthor("Mikalai Alimenkou")).thenReturn(singletonList(book));

        assertBooksByAuthor(" \t Mikalai \n Alimenkou \t ", book);
    }

    private void assertBooksByAuthor(String author, Book book) {
        assertThat(bookService.findBooksByAuthor(author)).contains(book);
    }

    private void assertNoBooksFound(String author) {
        assertThat(bookService.findBooksByAuthor(author)).isEmpty();
    }
}
