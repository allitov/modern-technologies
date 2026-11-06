package io.allitov.cadr.service;

import io.allitov.cadr.domain.Asset;

/**
 * Строка таблицы «Техника»: единица техники и текущий держатель.
 *
 * @param asset  единица техники;
 * @param holder ФИО фотографа, если техника сейчас на съёмке,
 *               или пустая строка, если в стойке.
 */
public record AssetRow(Asset asset, String holder) {
}
