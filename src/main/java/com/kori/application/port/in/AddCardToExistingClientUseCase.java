package com.kori.application.port.in;

import com.kori.application.command.AddCardToExistingClientCommand;
import com.kori.application.result.AddCardToExistingClientResult;

public interface AddCardToExistingClientUseCase {
    AddCardToExistingClientResult execute(AddCardToExistingClientCommand command);
}
