package com.ledger.personal.ui.screens.list;

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
public final class TransactionListViewModel_Factory implements Factory<TransactionListViewModel> {
  private final Provider<TransactionRepository> repositoryProvider;

  private TransactionListViewModel_Factory(Provider<TransactionRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public TransactionListViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static TransactionListViewModel_Factory create(
      Provider<TransactionRepository> repositoryProvider) {
    return new TransactionListViewModel_Factory(repositoryProvider);
  }

  public static TransactionListViewModel newInstance(TransactionRepository repository) {
    return new TransactionListViewModel(repository);
  }
}
