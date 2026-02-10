package com.kori.application.port.out.query;

import com.kori.application.query.BackofficeLookupItem;
import com.kori.application.query.BackofficeLookupQuery;

import java.util.List;

public interface BackofficeLookupReadPort {
    List<BackofficeLookupItem> search(BackofficeLookupQuery query);
}
