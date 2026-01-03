package iuh.fit.goat.component.award;

import iuh.fit.goat.enumeration.RatingType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AwardStrategyFactory {
    private final Map<RatingType, AwardStrategy> strategies;

    public AwardStrategyFactory(List<AwardStrategy> strategies) {
        this.strategies = strategies.stream().collect(Collectors.toMap(AwardStrategy::getType, s -> s));
    }

    public AwardStrategy getStrategy(RatingType type) {
        return this.strategies.get(type);
    }
}
