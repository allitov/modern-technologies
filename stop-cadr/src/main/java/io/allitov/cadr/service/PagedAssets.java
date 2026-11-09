package io.allitov.cadr.service;

import java.util.List;

/**
 * Страница списка «Техника».
 *
 * @param items   строки текущей страницы.
 * @param hasNext есть ли следующая страница.
 */
public record PagedAssets(List<AssetRow> items, boolean hasNext) {
}
