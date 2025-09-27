package cz.muni.xmichalk.Traverser.DTO;

public interface IDTO<T> {
    T toDomainModel();

    IDTO<T> from(T domainModel);

}
