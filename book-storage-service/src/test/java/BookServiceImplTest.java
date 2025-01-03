import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.modsen.bookstorageservice.domain.Book;
import com.modsen.bookstorageservice.domain.exception.DuplicateResourceException;
import com.modsen.bookstorageservice.domain.exception.ResourceNotFoundException;
import com.modsen.bookstorageservice.repository.BookRepository;
import com.modsen.bookstorageservice.service.RabbitService;
import com.modsen.bookstorageservice.service.impl.BookServiceImpl;
import com.modsen.bookstorageservice.dto.BookDto;
import com.modsen.bookstorageservice.mapper.BookMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


@ExtendWith(MockitoExtension.class)
@DisplayName("BookService tests")
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private RabbitService rabbitService;


    @InjectMocks
    private BookServiceImpl bookService;

    private static Book book;
    private static BookDto bookDto;

    @BeforeAll
    static void beforeAll() {
        book = new Book();
        book.setId(1L);
        book.setIsbn("123456789");
        book.setTitle("Test Book");
        book.setAuthor("Test Author");

        bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setIsbn("123456789");
        bookDto.setTitle("Test Book");
        bookDto.setAuthor("Test Author");
    }

    @Test
    @DisplayName("testGetBookByIdSuccess")
    void testGetBookByIdSuccess() {
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(bookMapper.toDto(book)).thenReturn(bookDto);

        BookDto result = bookService.getBookById(book.getId());

        assertNotNull(result);
        assertEquals(bookDto.getId(), result.getId());
        verify(bookRepository, times(1)).findById(book.getId());
    }

    @Test
    @DisplayName("testGetBookByIdNotFound")
    void testGetBookByIdNotFound() {

        when(bookRepository.findById(book.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookService.getBookById(bookDto.getId()));
        verify(bookRepository, times(1)).findById(bookDto.getId());
    }

    @Test
    @DisplayName("testCreateBookDuplicate")
    void testCreateBookDuplicate() {
        when(bookMapper.toEntity(bookDto)).thenReturn(book);
        when(bookRepository.findByIsbn(book.getIsbn())).thenReturn(Optional.of(book));

        assertThrows(DuplicateResourceException.class, () -> bookService.createBook(bookDto));
        verify(bookRepository, times(1)).findByIsbn("123456789");
    }

    @Test
    @DisplayName("testUpdateBookSuccess")
    void testUpdateBookSuccess() {
        when(bookMapper.toEntity(bookDto)).thenReturn(book);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(bookRepository.save(book)).thenReturn(book);

        BookDto result = bookService.updateBook(bookDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    @DisplayName("testGetBookByIdSuccess")
    void testUpdateBook_NotFound() {
        when(bookMapper.toEntity(bookDto)).thenReturn(book);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookService.updateBook(bookDto));
        verify(bookRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("deleteBookBookNotFound")
    void deleteBookBookNotFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookService.deleteBook(1L));

        verify(rabbitService, never()).addDeleteBook(anyLong());
        verify(bookRepository, never()).delete(any(Book.class));
    }


    @Test
    @DisplayName("testGetBookByIsbnSuccess")
    void testGetBookByIsbnSuccess() {
        when(bookRepository.findByIsbn(book.getIsbn())).thenReturn(Optional.of(book));
        when(bookMapper.toDto(book)).thenReturn(bookDto);

        BookDto result = bookService.getBookByIsbn(bookDto.getIsbn());

        assertNotNull(result);
        assertEquals(bookDto.getIsbn(), result.getIsbn());
        verify(bookRepository, times(1)).findByIsbn(book.getIsbn());
    }

    @Test
    @DisplayName("testGetBookByIsbnNotFound")
    void testGetBookByIsbnNotFound() {
        String isbn = "1234567890";
        when(bookRepository.findByIsbn(isbn)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookService.getBookByIsbn(isbn));
        verify(bookRepository, times(1)).findByIsbn(isbn);
    }

    @Test
    @DisplayName("testGetAllBooksWhenBooksExist")
    void testGetAllBooksWhenBooksExist() {
        List<Book> books = List.of(
                new Book(1L, "978-3-16-1485678410-0", "A Clockwork Orange", "Fantasy", "A thought-provoking novel about free will and morality.",
                        "Anthony Burgess"),
                new Book(2L,"978-0-7432-7356-5", "The Great Gatsby", "Classic", "A novel about the American dream and the jazz age.",
                        "F. Scott Fitzgerald")
        );
        List<BookDto> bookDtos = List.of(
                new BookDto(1L, "978-3-16-1485678410-0", "A Clockwork Orange", "Fantasy", "A thought-provoking novel about free will and morality.",
                        "Anthony Burgess"),
                new BookDto(2L,"978-0-7432-7356-5", "The Great Gatsby", "Classic", "A novel about the American dream and the jazz age.",
                        "F. Scott Fitzgerald")
        );
        when(bookRepository.findAll()).thenReturn(books);
        when(bookMapper.toDto(books)).thenReturn(bookDtos);

        List<BookDto> result = bookService.getAllBooks();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("978-3-16-1485678410-0", result.get(0).getIsbn());
        assertEquals("978-0-7432-7356-5", result.get(1).getIsbn());
        verify(bookRepository, times(1)).findAll();
        verify(bookMapper, times(1)).toDto(books);
    }


    @Test
    @DisplayName("testGetAllBooksWhenNoBooksExist")
    void testGetAllBooksWhenNoBooksExist() {
        when(bookRepository.findAll()).thenReturn(Collections.emptyList());
        when(bookMapper.toDto(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<BookDto> result = bookService.getAllBooks();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(bookRepository, times(1)).findAll();
        verify(bookMapper, times(1)).toDto(Collections.emptyList());
    }
}
