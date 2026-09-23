package com.ledger.personal.ui.screens.edit;

import androidx.lifecycle.SavedStateHandle;
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
public final class EditTransactionViewModel_Factory implements Factory<EditTransactionViewModel> {
  private final Provider<TransactionRepository> repositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private EditTransactionViewModel_Factory(Provider<TransactionRepository> repositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.repositoryProvider = repositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public EditTransactionViewModel get() {
    return newInstance(repositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static EditTransactionViewModel_Factory create(
      Provider<TransactionRepository> repositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new EditTransactionViewModel_Factory(repositoryProvider, savedStateHandleProvider);
  }

  public static EditTransactionViewModel newInstance(TransactionRepository repository,
      SavedStateHandle savedStateHandle) {
    return new EditTransactionViewModel(repository, savedStateHandle);
  }
}
