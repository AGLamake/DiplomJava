var App = {
    /** Подсветка сработавшего датчика на 2 секунды. */
    flash: function (id) {
        var button = document.getElementById(id);
        if (!button) {
            return;
        }
        button.classList.add('fired');
        setTimeout(function () {
            button.classList.remove('fired');
        }, 2000);
    },

    /** Прокрутка журнала к последней записи — только если журнал был перерисован. */
    scrollJournal: function () {
        var journal = document.getElementById('monitorForm:journal');
        if (journal && !journal.dataset.scrolled) {
            journal.scrollTop = journal.scrollHeight;
            journal.dataset.scrolled = '1';
        }
    }
};
