package jp.co.metateam.library.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.co.metateam.library.model.BookMst;
import jp.co.metateam.library.model.BookMstDto;
import jp.co.metateam.library.repository.BookMstRepository;

@Service
public class BookMstService {

    private final BookMstRepository bookMstRepository;

    @Autowired
    public BookMstService(BookMstRepository bookMstRepository) {
        this.bookMstRepository = bookMstRepository;
    }

    // 一覧取得（論理削除されていない書籍のみ）
    public List<BookMstDto> findAvailableWithStockCount() {
        List<BookMst> books = this.bookMstRepository.findAllNotDeleted(); // ← 修正済み
        List<BookMstDto> bookMstDtoList = new ArrayList<>();

        for (BookMst book : books) {
            BookMstDto dto = new BookMstDto();
            dto.setId(book.getId());
            dto.setIsbn(book.getIsbn());
            dto.setTitle(book.getTitle());
            bookMstDtoList.add(dto);
        }

        return bookMstDtoList;
    }

    public String searchIsbn(String isbn) {
        Optional<BookMst> bookMstOptional = bookMstRepository.selectByisbn(isbn);
        return bookMstOptional.map(BookMst::getIsbn).orElse(null);
    }

    public String searchActiveIsbn(String isbn) {
        Optional<BookMst> bookMstOptional = bookMstRepository.selectActiveByIsbn(isbn);
        return bookMstOptional.map(BookMst::getIsbn).orElse(null);
    }

    @Transactional
    public void save(BookMstDto bookmstDto) {
        BookMst bookMst = new BookMst();
        bookMst.setTitle(bookmstDto.getTitle());
        bookMst.setIsbn(bookmstDto.getIsbn());
        bookMst.setDeletedFlag(false); // 明示的に未削除として保存
        this.bookMstRepository.save(bookMst);
    }

    public BookMstDto findById(Long id) {
        BookMst entity = bookMstRepository.findById(id).orElse(null);
        if (entity == null || entity.getDeletedAt() != null)
            return null;

        BookMstDto dto = new BookMstDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setIsbn(entity.getIsbn());
        return dto;
    }

    public void update(BookMstDto bookMstDto) {
        BookMst bookMst = bookMstRepository.findById(bookMstDto.getId())
                .orElseThrow(() -> new RuntimeException("書籍が見つかりません"));

        bookMst.setTitle(bookMstDto.getTitle());
        bookMst.setIsbn(bookMstDto.getIsbn());

        this.bookMstRepository.save(bookMst);
    }

    @Transactional
    public void deleteById(Long id) {
        Optional<BookMst> bookOpt = bookMstRepository.findById(id);

        if (bookOpt.isEmpty()) {
            throw new IllegalArgumentException("書籍が存在しません。");
        }

        BookMst book = bookOpt.get();

        if (Boolean.TRUE.equals(book.getDeletedFlag())) {
            throw new IllegalArgumentException("この書籍は既に削除されています。");
        }

        book.setDeletedFlag(true); // フラグを立てて論理削除
        book.setDeletedAt(Timestamp.from(Instant.now())); 
        bookMstRepository.save(book);
    }

}
