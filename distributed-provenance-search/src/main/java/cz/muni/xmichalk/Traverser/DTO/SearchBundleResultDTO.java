package cz.muni.xmichalk.Traverser.DTO;

import cz.muni.xmichalk.Traverser.Models.ECredibility;
import cz.muni.xmichalk.Traverser.Models.SearchBundleResult;

import java.util.List;

public class SearchBundleResultDTO implements IDTO<SearchBundleResult> {
    public List<QualifiedNameDTO> resultIds;
    public List<ConnectorNodeDTO> connectors;
    public ECredibility credibility;

    public SearchBundleResultDTO() {
    }

    @Override
    public SearchBundleResult toDomainModel() {
        var res = new SearchBundleResult();
        res.results = this.resultIds.stream().map(QualifiedNameDTO::toDomainModel).toList();
        res.connectors = this.connectors.stream().map(ConnectorNodeDTO::toDomainModel).toList();
        res.credibility = this.credibility;
        return res;
    }

    @Override
    public SearchBundleResultDTO from(final SearchBundleResult domainModel) {
        this.resultIds = domainModel.results.stream().map(qName -> new QualifiedNameDTO().from(qName)).toList();
        this.connectors = domainModel.connectors.stream().map(connector -> new ConnectorNodeDTO().from(connector)).toList();
        this.credibility = domainModel.credibility;
        return this;
    }
}
