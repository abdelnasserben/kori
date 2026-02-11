package com.kori.query.port.in;

import com.kori.query.model.BackofficeLookupItem;
import com.kori.query.model.BackofficeLookupQuery;

import java.util.List;

public interface BackofficeLookupQueryUseCase {
    List<BackofficeLookupItem> search(BackofficeLookupQuery query);
}
