package org.imozerov.streetartview;

import org.imozerov.streetartview.storage.DataSource;
import org.imozerov.streetartview.ui.add.AddArtObjectPresenter;
import org.imozerov.streetartview.ui.detail.DetailArtObjectActivity;
import org.imozerov.streetartview.ui.detail.ImageViewActivity;
import org.imozerov.streetartview.ui.explore.ArtListPresenter;
import org.imozerov.streetartview.ui.explore.ExploreArtActivity;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by imozerov on 06.02.16.
 */
@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {
    void inject(ExploreArtActivity exploreArtActivity);
    void inject(ArtListPresenter artListPresenter);
    void inject(DetailArtObjectActivity detailArtObjectActivity);
    void inject(ImageViewActivity imageViewActivity);
    void inject(DataSource dataSource);
    void inject(AddArtObjectPresenter addArtObjectPresenter);
}
