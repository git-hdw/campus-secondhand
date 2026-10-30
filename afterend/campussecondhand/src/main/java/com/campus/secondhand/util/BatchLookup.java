package com.campus.secondhand.util;
import com.campus.secondhand.entity.*;
import com.campus.secondhand.repository.*;
import java.util.*;
import java.util.stream.Collectors;
public final class BatchLookup {
    private BatchLookup() {}
    public static Map<Long, User> users(UserRepository repository, Collection<Long> ids) {
        var keys = ids.stream().filter(Objects::nonNull).distinct().toList();
        return keys.isEmpty() ? Map.of() : repository.selectBatchIds(keys).stream().collect(Collectors.toMap(User::getId, u -> u));
    }
    public static Map<Long, Product> products(ProductRepository repository, Collection<Long> ids) {
        var keys = ids.stream().filter(Objects::nonNull).distinct().toList();
        return keys.isEmpty() ? Map.of() : repository.selectBatchIds(keys).stream().collect(Collectors.toMap(Product::getId, p -> p));
    }
}
