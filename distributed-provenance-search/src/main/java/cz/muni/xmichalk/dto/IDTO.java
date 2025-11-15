package cz.muni.xmichalk.dto;

public interface IDTO<T> {
    T toDomainModel();

    IDTO<T> from(T domainModel);

}
