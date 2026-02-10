package com.kori.application.query;

import java.util.List;

public record QueryPage<T>(List<T> items, String nextCursor, boolean hasMore) {
}
