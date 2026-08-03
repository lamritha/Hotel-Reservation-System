package com.hotelreservation.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Reusable pagination support for sortable JavaFX tables.
 *
 * <p>The complete result set is kept separately from the visible page so
 * sorting applies to all results rather than only the currently displayed
 * rows.</p>
 *
 * @param <T> table row type
 */
public final class TablePaginationSupport<T> {

    private final TableView<T> table;
    private final Label resultLabel;
    private final Label pageLabel;
    private final Button previousButton;
    private final Button nextButton;
    private final int rowsPerPage;
    private final String singularDescription;
    private final String pluralDescription;

    private final ObservableList<T> sourceItems =
            FXCollections.observableArrayList();
    private final SortedList<T> sortedItems =
            new SortedList<>(sourceItems);

    private int currentPage;
    private boolean updatingTable;

    public TablePaginationSupport(
            TableView<T> table,
            Label resultLabel,
            Label pageLabel,
            Button previousButton,
            Button nextButton,
            int rowsPerPage,
            String singularDescription,
            String pluralDescription
    ) {
        this.table = Objects.requireNonNull(table, "table");
        this.resultLabel = Objects.requireNonNull(
                resultLabel,
                "resultLabel"
        );
        this.pageLabel = Objects.requireNonNull(
                pageLabel,
                "pageLabel"
        );
        this.previousButton = Objects.requireNonNull(
                previousButton,
                "previousButton"
        );
        this.nextButton = Objects.requireNonNull(
                nextButton,
                "nextButton"
        );

        if (rowsPerPage <= 0) {
            throw new IllegalArgumentException(
                    "Rows per page must be greater than zero."
            );
        }

        this.rowsPerPage = rowsPerPage;
        this.singularDescription = Objects.requireNonNull(
                singularDescription,
                "singularDescription"
        );
        this.pluralDescription = Objects.requireNonNull(
                pluralDescription,
                "pluralDescription"
        );

        sortedItems.comparatorProperty().bind(
                table.comparatorProperty()
        );

        table.setSortPolicy(ignored -> {
            if (!updatingTable) {
                currentPage = 0;
                updateDisplayedPage();
            }
            return true;
        });

        updateDisplayedPage();
    }

    public void setItems(Collection<? extends T> items) {
        sourceItems.setAll(
                items == null ? List.of() : items
        );
        currentPage = 0;
        updateDisplayedPage();
    }

    public List<T> getAllItems() {
        return List.copyOf(sortedItems);
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateDisplayedPage();
        }
    }

    public void nextPage() {
        if (currentPage + 1 < getPageCount()) {
            currentPage++;
            updateDisplayedPage();
        }
    }

    public void refresh() {
        updateDisplayedPage();
    }

    private void updateDisplayedPage() {
        int pageCount = getPageCount();

        if (currentPage >= pageCount) {
            currentPage = Math.max(0, pageCount - 1);
        }

        int fromIndex = currentPage * rowsPerPage;
        int toIndex = Math.min(
                fromIndex + rowsPerPage,
                sortedItems.size()
        );

        List<T> visibleItems =
                fromIndex >= sortedItems.size()
                        ? List.of()
                        : sortedItems.subList(
                                fromIndex,
                                toIndex
                        );

        updatingTable = true;
        try {
            table.getSelectionModel().clearSelection();
            table.setItems(
                    FXCollections.observableArrayList(
                            visibleItems
                    )
            );
        } finally {
            updatingTable = false;
        }

        int resultCount = sourceItems.size();
        resultLabel.setText(
                resultCount
                        + " "
                        + (resultCount == 1
                        ? singularDescription
                        : pluralDescription)
        );
        pageLabel.setText(
                "Page "
                        + (currentPage + 1)
                        + " of "
                        + pageCount
        );

        previousButton.setDisable(currentPage == 0);
        nextButton.setDisable(
                currentPage + 1 >= pageCount
        );
    }

    private int getPageCount() {
        return Math.max(
                1,
                (int) Math.ceil(
                        sourceItems.size()
                                / (double) rowsPerPage
                )
        );
    }
}
