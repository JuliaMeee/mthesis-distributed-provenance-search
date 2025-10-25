package cz.muni.xmichalk.DTO;

public interface IDTO<T> {
    T toDomainModel();

    IDTO<T> from(T domainModel);

}
