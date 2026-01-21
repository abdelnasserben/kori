package com.kori.application.port.in;

import com.kori.application.command.PayByCardCommand;
import com.kori.application.result.PayByCardResult;

public interface PayByCardUseCase {
    PayByCardResult execute(PayByCardCommand command);
}
