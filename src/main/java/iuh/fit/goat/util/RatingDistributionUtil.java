package iuh.fit.goat.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class RatingDistributionUtil {

    private RatingDistributionUtil() {}

    public static Map<Integer, Integer> build(List<Object[]> raw) {

        Map<Integer, Integer> dist = IntStream
                .rangeClosed(1, 5)
                .boxed()
                .collect(
                        Collectors.toMap(
                            i -> i,
                            i -> 0,
                            (a, b) -> a,
                            LinkedHashMap::new
                        )
                );

        long total = raw.stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();

        if (total == 0) return dist;

        for (Object[] row : raw) {
            int star = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            dist.put(star, percent(count, total));
        }

        normalize(dist);
        return dist;
    }

    private static int percent(long count, long total) {
        return (int) Math.round(count * 100.0 / total);
    }

    private static void normalize(Map<Integer, Integer> dist) {
        int sum = dist.values().stream().mapToInt(i -> i).sum();
        if (sum != 100) {
            dist.put(5, dist.get(5) + (100 - sum));
        }
    }
}

