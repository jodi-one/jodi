databaseChangeLog {
  changeSet(id: '''1602509949566-1''', author: '''dukevanleeuwen (generated)''') {
    createTable(tableName: '''HEARTBEAT''') {
      column(name: '''CREATED''', type: '''date''') {
        constraints(nullable: false)
      }
    }
  }

  changeSet(id: '''1602509949566-2''', author: '''dukevanleeuwen (generated)''') {
    createView(fullDefinition: true, viewName: '''JODI_STORAGE''') {
      ''' CREATE OR REPLACE FORCE VIEW JODI_STORAGE (MB) AS select sum(bytes) / 1024 / 1024 MB from dba_segments where tablespace_name = 'DATA' '''
    }
  }

}
