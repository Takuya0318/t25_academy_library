package jp.co.metateam.library.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jp.co.metateam.library.model.Account;
import jp.co.metateam.library.model.AccountDto;
import jp.co.metateam.library.model.BookMst;
import jp.co.metateam.library.model.BookMstDto;
import jp.co.metateam.library.service.BookMstService;
import lombok.extern.log4j.Log4j2;

/**
 * 書籍関連クラス
 */
@Log4j2
@Controller
public class BookController {
    
    private final BookMstService bookMstService;

    @Autowired
    public BookController(BookMstService bookMstService){
        this.bookMstService = bookMstService;
    }

    @GetMapping("/book/index")
    public String index(Model model) {
        // 書籍を全件取得
        List<BookMstDto> bookMstList = this.bookMstService.findAvailableWithStockCount();
        
        model.addAttribute("bookMstList", bookMstList);

        return "book/index";
    }

    @GetMapping("/book/add")
    public String add(Model model) {
        if (!model.containsAttribute("bookMstDto")) {
            model.addAttribute("bookMstDto", new BookMstDto());
        }

        return "book/add";
    }

    
    @PostMapping("/book/add")
    public String register(@ModelAttribute BookMstDto bookMstDto, BindingResult result, RedirectAttributes ra) {
    try {
       boolean hasError = false;
       String isbnHolder = bookMstDto.getIsbn();
 
       if (bookMstDto.getTitle() == null || bookMstDto.getTitle().trim().isEmpty()) {
           result.rejectValue("title", "error.title.required", "書籍名は必須です");
           hasError = true;
       }
       
       if (bookMstDto.getTitle() != null && bookMstDto.getTitle().length() > 255) {
           result.rejectValue("title", "error.title.length", "書籍名は255文字以内で入力してください");
           hasError = true;
       }
   
       if (bookMstDto.getIsbn() == null || bookMstDto.getIsbn().trim().isEmpty()) {
           result.rejectValue("isbn", "error.isbn.required", "ISBNは必須です");
           hasError = true;
       }
       
       if (bookMstDto.getIsbn() != null && !bookMstDto.getIsbn().isEmpty() && bookMstDto.getIsbn().length() != 13) {
           result.rejectValue("isbn", "error.isbn.length", "ISBNは13桁で入力してください");
           hasError = true;
       }
       
       if (bookMstDto.getIsbn() != null && !bookMstDto.getIsbn().isEmpty() && !bookMstDto.getIsbn().matches("^[0-9]+$")) {
           result.rejectValue("isbn", "error.isbn.hankaku", "ISBNは半角で入力してください");
           hasError = true;
       }
       if (bookMstService.searchIsbn(isbnHolder) != null && !bookMstService.searchIsbn(isbnHolder).isEmpty()) {
        result.rejectValue("isbn", "error.isbn.duplicate", "このISBNは既に登録済みです");
        hasError = true;
    }
       
       if (hasError) {
           throw new Exception("バリデーションエラー");
       }
       
       bookMstService.save(bookMstDto);
       return "redirect:/book/index";
        } catch (Exception e) {
       log.error("書籍登録エラー: {}", e.getMessage());
 
       ra.addFlashAttribute("bookMstDto", bookMstDto);
       ra.addFlashAttribute("org.springframework.validation.BindingResult.bookMstDto", result);
       return "redirect:/book/add";
   }
   }
   // 書籍の編集画面（編集フォーム）を表示する処理のことを GET メソッドという
@GetMapping("/book/edit/{id}") // URLの {id} 部分を取り出して処理するマッピング（例：/book/edit/5）
public String edit(@PathVariable Long id, Model model, RedirectAttributes ra) {

    // サービスクラスを使って、指定されたIDの書籍をデータベースから取得する
    BookMstDto bookMstDto = bookMstService.findById(id);

    // もし該当の書籍が見つからなければ（nullの場合） → 一覧画面にリダイレクトし、メッセージ表示
    if (bookMstDto == null) {
        ra.addFlashAttribute("message", "この書籍は削除されました"); // 一時的なメッセージとして追加
        return "redirect:/book/index"; // 書籍一覧ページへリダイレクト
    }

    // 編集画面に渡すデータとして、取得した書籍情報を model に格納
    model.addAttribute("bookMstDto", bookMstDto);

    // 編集画面（book/edit.html）を表示
    return "book/edit";
}
// 編集画面で入力された内容を受け取って保存する処理のことを POST メソッドという
@PostMapping("/book/edit")
public String edit(@Valid @ModelAttribute BookMstDto bookMstDto, BindingResult result, RedirectAttributes ra) {
    try {
        boolean errTitleFlg = false; // 書籍名のバリデーションエラーフラグ
        boolean errIsbnFlg = false;  // ISBNのバリデーションエラーフラグ

        // 現在DBにある元のデータを取得
        BookMstDto original = bookMstService.findById(bookMstDto.getId());

        // 元データが存在しない（削除済み）場合は一覧に戻してメッセージ表示
        if (original == null) {
            ra.addFlashAttribute("message", "この書籍は削除されました");
            return "redirect:/book/index";
        }

        // 入力されたタイトルとISBNを変数に代入
        String titleExist = bookMstDto.getTitle();
        String isbnExist = bookMstDto.getIsbn();

        // 元データと比べて、書籍名・ISBNに変更があったかどうかを判定
        boolean isTitleChanged = !original.getTitle().equals(titleExist);
        boolean isIsbnChanged = !original.getIsbn().equals(isbnExist);

        // どちらにも変更がない場合 → メッセージを表示して編集画面に戻る
        if (!isTitleChanged && !isIsbnChanged) {
            ra.addFlashAttribute("message", "変更がありませんでした");
            return "redirect:/book/edit/" + bookMstDto.getId(); // 同じ編集画面に戻る
        }

        // タイトルが変更された場合のみバリデーションを実行
        if (isTitleChanged) {
            if (titleExist == null || titleExist.isEmpty()) {
                result.rejectValue("title", "error.value", "書籍名は必須です");
                errTitleFlg = true;
            } else if (titleExist.length() > 255) {
                result.rejectValue("title", "error.value", "書籍名は255文字以内で入力してください");
                errTitleFlg = true;
            }
        }

        // ISBNが変更された場合のみバリデーションを実行
        if (isIsbnChanged) {
            if (isbnExist == null || isbnExist.isEmpty()) {
                result.rejectValue("isbn", "error.value", "ISBNは必須です");
                errIsbnFlg = true;
            }
            if (isbnExist.length() != 13) {
                result.rejectValue("isbn", "error.value", "ISBNは13文字で入力してください");
                errIsbnFlg = true;
            }
            if (!isbnExist.matches("^[\\p{ASCII}]*$")) {
                result.rejectValue("isbn", "error.value", "ISBNは半角文字で入力してください");
                errIsbnFlg = true;
            }

            // 重複チェック：同じISBNが他の書籍にすでに登録されていないかを確認
            if (bookMstService.searchIsbn(isbnExist) != null &&
                !bookMstService.searchIsbn(isbnExist).isEmpty()) {
                result.rejectValue("isbn", "error.value", "既に登録済みのISBNです");
                errIsbnFlg = true;
            }
        }

        // バリデーションエラーが1つでもあった場合は例外を投げる（→ catch に進む）
        if (errTitleFlg || errIsbnFlg) {
            throw new Exception("Validation failed.");
        }

        // バリデーションOK → 更新処理をサービスに依頼
        bookMstService.update(bookMstDto);

        // 完了メッセージを表示し、一覧画面に戻る
        ra.addFlashAttribute("message", "変更されました");
        return "redirect:/book/index";

    } catch (Exception e) {
        // 例外が発生した場合（バリデーションエラーなど）

        log.error("Error during book edit: " + e.getMessage()); // エラーログ出力

        // 入力値とエラー内容を再度渡して、編集画面に戻る
        ra.addFlashAttribute("bookMstDto", bookMstDto);
        ra.addFlashAttribute("org.springframework.validation.BindingResult.bookMstDto", result);

        return "book/edit";
    }
}
}