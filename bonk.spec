Name:           bonk
Version:        {{VERSION}}
Release:        1%{?dist}
Summary:        Soundboard for Linux
License:        MIT
URL:            https://github.com/kvxd/bonk
Source0:        https://github.com/kvxd/bonk/releases/download/v%{version}/%{name}-%{version}.tar.gz

%global debug_package %{nil}

Requires:       mpv
Requires:       pulseaudio-utils
Requires:       ffmpeg-free

%description
Soundboard for Linux.

%prep
%setup -q

%build

%install
mkdir -p %{buildroot}/opt/%{name}
mkdir -p %{buildroot}%{_bindir}
mkdir -p %{buildroot}%{_datadir}/applications
mkdir -p %{buildroot}%{_datadir}/icons/hicolor/512x512/apps

cp -r bin lib %{buildroot}/opt/%{name}/

ln -s /opt/%{name}/bin/%{name} %{buildroot}%{_bindir}/%{name}

cp bonk.desktop %{buildroot}%{_datadir}/applications/

cp bonk.png %{buildroot}%{_datadir}/icons/hicolor/512x512/apps/%{name}.png

chmod +x %{buildroot}/opt/%{name}/bin/%{name}
find %{buildroot}/opt/%{name} -type f -name "*.so" -exec chmod +x {} +

%post
/bin/touch --no-create %{_datadir}/icons/hicolor &>/dev/null || :

%postun
if [ $1 -eq 0 ] ; then
    /bin/touch --no-create %{_datadir}/icons/hicolor &>/dev/null
    /usr/bin/gtk-update-icon-cache %{_datadir}/icons/hicolor &>/dev/null || :
fi

%posttrans
/usr/bin/gtk-update-icon-cache %{_datadir}/icons/hicolor &>/dev/null || :

%files
%defattr(-,root,root,-)
%attr(0755,root,root) /opt/%{name}/bin/%{name}
/opt/%{name}
%{_bindir}/%{name}
%{_datadir}/applications/%{name}.desktop
%{_datadir}/icons/hicolor/512x512/apps/%{name}.png

%changelog
{{CHANGELOG}}