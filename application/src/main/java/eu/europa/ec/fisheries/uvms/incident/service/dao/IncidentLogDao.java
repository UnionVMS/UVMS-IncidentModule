package eu.europa.ec.fisheries.uvms.incident.service.dao;

import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.IncidentLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class IncidentLogDao {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentLogDao.class);

    @PersistenceContext
    private EntityManager em;


    public void save(IncidentLog entity) {
        this.em.persist(entity);
        LOG.info("New Incident Log created with ID: " + entity.getId());
    }

    public List<IncidentLog> findAllByIncidentId(long incidentId) {
        TypedQuery<IncidentLog> query = em.createNamedQuery(IncidentLog.FIND_ALL_BY_INCIDENT_ID, IncidentLog.class);
        query.setParameter("incidentId", incidentId);
        return query.getResultList();
    }

    public List<IncidentLog> findAllByIncidentId(List<Long> incidentId) {
        TypedQuery<IncidentLog> query = em.createNamedQuery(IncidentLog.FIND_ALL_BY_INCIDENT_ID, IncidentLog.class);
        query.setParameter("incidentId", incidentId);
        return query.getResultList();
    }
}
