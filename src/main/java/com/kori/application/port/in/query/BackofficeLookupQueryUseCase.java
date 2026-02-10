package com.kori.application.port.in.query;

import com.kori.application.query.BackofficeLookupItem;
import com.kori.application.query.BackofficeLookupQuery;

import java.util.List;

public interface BackofficeLookupQueryUseCase {
    List<BackofficeLookupItem> search(BackofficeLookupQuery query);
}
