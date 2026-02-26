package com.nebula.catalog.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "folders")
@DiscriminatorValue("FOLDER")
public class Folder extends CatalogItem {
}
