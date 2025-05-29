
document.addEventListener("DOMContentLoaded", function () {
    const msg = document.getElementById("flash-message")?.dataset.message;
    if (msg) {
        alert(msg); // 成功メッセージ
    }

    const errMsg = document.getElementById("flash-error-message")?.dataset.errorMessage;
    if (errMsg) {
        alert("エラー: " + errMsg); // 例：「この書籍は既に削除されています。」
    }
});

window.addEventListener("DOMContentLoaded", function () {
    const infoDialog = document.getElementById("infoDialog");
    if (infoDialog) {
        infoDialog.showModal();
    }
});

function closeInfoDialog() {
    const infoDialog = document.getElementById("infoDialog");
    if (infoDialog) {
        infoDialog.close();
    }
}