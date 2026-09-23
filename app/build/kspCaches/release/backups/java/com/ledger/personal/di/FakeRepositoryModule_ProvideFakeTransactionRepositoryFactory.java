package com.ledger.personal.di;

import com.ledger.personal.data.repository.FakeTransactionRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class FakeRepositoryModule_ProvideFakeTransactionRepositoryFactory implements Factory<FakeTransactionRepository> {
  @Override
  public FakeTransactionRepository get() {
    return provideFakeTransactionRepository();
  }

  public static FakeRepositoryModule_ProvideFakeTransactionRepositoryFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FakeTransactionRepository provideFakeTransactionRepository() {
    return Preconditions.checkNotNullFromProvides(FakeRepositoryModule.INSTANCE.provideFakeTransactionRepository());
  }

  private static final class InstanceHolder {
    static final FakeRepositoryModule_ProvideFakeTransactionRepositoryFactory INSTANCE = new FakeRepositoryModule_ProvideFakeTransactionRepositoryFactory();
  }
}
