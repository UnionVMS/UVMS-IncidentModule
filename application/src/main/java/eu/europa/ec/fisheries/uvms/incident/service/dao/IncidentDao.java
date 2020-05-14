package eu.europa.ec.fisheries.uvms.incident.service.dao;

import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.enums.StatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Stateless
public class IncidentDao {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentDao.class);

    @PersistenceContext
    private EntityManager em;


    public Incident save(Incident entity) {
        this.em.persist(entity);
        LOG.info("New Incident created with ID: " + entity.getId());
        return entity;
    }
    public Incident findById(long id) {
        Incident incident = this.em.find(Incident.class, id);
        if (incident == null) {
            throw new EntityNotFoundException("Can't find Incident for Id: " + id);
        }
        return incident;
    }

    public List<Incident> findUnresolvedIncidents() {
        TypedQuery<Incident> query = em.createNamedQuery(Incident.FIND_ALL_EXCLUDE_STATUS, Incident.class);
        query.setParameter("status", StatusEnum.RESOLVED);
        return query.getResultList();
    }

    public List<Incident> findByStatusAndUpdatedSince() {
        TypedQuery<Incident> query = em.createNamedQuery(Incident.FIND_BY_STATUS_AND_UPDATED_SINCE, Incident.class);
        query.setParameter("status", StatusEnum.RESOLVED);
        query.setParameter("updatedSince", Instant.now().minus(12, ChronoUnit.HOURS));
        return query.getResultList();
    }

    public List<Incident> findByAssetId(String assetId) {
        TypedQuery<Incident> query = em.createNamedQuery(Incident.FIND_BY_ASSET_ID, Incident.class);
        query.setParameter("assetId", assetId);
        return query.getResultList();
    }

    public Incident findByTicketId(UUID ticketId) {
        try {
            TypedQuery<Incident> query = em.createNamedQuery(Incident.FIND_BY_TICKET_ID, Incident.class);
            query.setParameter("ticketId", ticketId);
            return query.getSingleResult();
        } catch (Exception e) {
            LOG.warn("No Ticket found to be updated. Ticket ID: {}", ticketId);
            return null;
        }
    }

    public Incident update(Incident entity) {
        entity.setUpdateDate(Instant.now());
        return em.merge(entity);
    }
}
