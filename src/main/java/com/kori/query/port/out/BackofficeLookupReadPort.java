package com.kori.query.port.out;

import com.kori.query.model.BackofficeLookupItem;
import com.kori.query.model.BackofficeLookupQuery;

import java.util.List;

public interface BackofficeLookupReadPort {
    List<BackofficeLookupItem> search(BackofficeLookupQuery query);
}
