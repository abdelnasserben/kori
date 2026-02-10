package com.kori.adapters.out.jpa.query;

import java.util.List;

public record QueryPage<T>(List<T> items, String nextCursor, boolean hasMore) {
}
