package com.finaegis.domain.account.query;

import lombok.Value;
import java.util.List;

@Value
public class GetAccountsByUserQuery {
    String userId;
}
