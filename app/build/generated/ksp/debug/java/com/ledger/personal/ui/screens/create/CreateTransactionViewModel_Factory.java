package com.ledger.personal.ui.screens.create;

import com.ledger.personal.domain.repository.TransactionRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class CreateTransactionViewModel_Factory implements Factory<CreateTransactionViewModel> {
  private final Provider<TransactionRepository> repositoryProvider;

  private CreateTransactionViewModel_Factory(Provider<TransactionRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public CreateTransactionViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static CreateTransactionViewModel_Factory create(
      Provider<TransactionRepository> repositoryProvider) {
    return new CreateTransactionViewModel_Factory(repositoryProvider);
  }

  public static CreateTransactionViewModel newInstance(TransactionRepository repository) {
    return new CreateTransactionViewModel(repository);
  }
}
